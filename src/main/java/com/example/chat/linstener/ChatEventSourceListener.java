package com.example.chat.linstener;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONStrFormatter;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

// 使用@Slf4j注解产生日志对象
@Slf4j
public class ChatEventSourceListener extends EventSourceListener {
    // 声明一个私有的PrintWriter对象writer，用于向客户端发送数据
    private PrintWriter writer;
    private CountDownLatch countDownLatch;

    // 构造函数接收一个PrintWriter类的实例作为参数，用于将数据写入客户端
//    public ChatEventSourceListener(PrintWriter writer) {
//        this.writer = writer;
//    }

    public ChatEventSourceListener(PrintWriter writer, CountDownLatch countDownLatch) {
        this.writer = writer;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onClosed(@NotNull EventSource eventSource) {
        log.info("OpenAI返回数据结束了 onClosed countDownLatch.countDown()");
        // 通知等待线程继续执行
        countDownLatch.countDown();
    }

    // 实现onEvent方法，用于处理接收到的事件
    @Override
    public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
        // 如果接收到的数据为"[DONE]"，则关闭writer对象
        if ("[DONE]".equals(data)) {
            writer.close();
            return;
        }
        // 将data解析为一个JSON对象
        JSONObject jsonObject = JSONUtil.parseObj(data);
        // 获取名为"choices"的JSON数组
        JSONArray choices = jsonObject.getJSONArray("choices");
        // 对于数组中的每一个元素，将其解析为一个choiceJSON对象
        choices.forEach(choice -> {
            JSONObject choiceJson = JSONUtil.parseObj(choice);
            // 获取名为"delta"的JSON对象
            JSONObject delta = choiceJson.getJSONObject("delta");
            // 如果delta对象的"role"字段不为空或"content"字段为空，则返回
            if (Objects.nonNull(delta.get("role")) || Objects.isNull(delta.getObj("content"))) return;
            // 将"content"字段的值写入writer对象中
            writer.write(delta.getStr("content"));
        });
        // 使用Lombok生成的logger对象记录JSONUtil.formatJsonStr(data)的值
        log.info(JSONUtil.formatJsonStr(data));
        // 刷新writer对象，将数据发送到客户端
        writer.flush();
    }

    @SneakyThrows
    @Override
    public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
        if (Objects.isNull(response)) {
            log.error("OpenAI sse连接异常:{}", t);
            eventSource.cancel();
        } else {
            ResponseBody body = response.body();
            if (Objects.nonNull(body)) {
                log.error("OpenAI  sse连接异常data：{}，异常：{}", body.string(), t);
            } else {
                log.error("OpenAI  sse连接异常data：{}，异常：{}", response, t);
            }
            eventSource.cancel();
        }
    }
}
