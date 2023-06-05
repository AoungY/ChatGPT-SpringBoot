package com.example.chat.controller;

import com.example.chat.config.DynamicKeyOpenAiAuthInterceptor;
import com.example.chat.linstener.ChatEventSourceListener;
import com.unfbx.chatgpt.OpenAiStreamClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.Message;
import com.unfbx.chatgpt.function.KeyRandomStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CountDownLatch;


@Slf4j
@RestController
@RequestMapping("/api/v1/")
public class DemoController {

//    @Value("${apiKeys}")
//    private List<String> apiKeys;

    @Autowired
    private Environment env;
    private float temperature = 0.5f;
    private String prompt = "";
    private static DynamicKeyOpenAiAuthInterceptor interceptor = new DynamicKeyOpenAiAuthInterceptor();


    @ResponseBody
// 注解用于将方法的返回值转换成 JSON 格式的数据
    @RequestMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
// 注解用于映射一个请求路径到该方法上，指定请求路径为 /chat，请求方法为 GET 或 POST，指定响应头的类型为 text/event-stream;charset=UTF-8
    public void getDate(@RequestBody List<Map<String, String>> listMap, HttpServletResponse response, HttpServletRequest req) throws Exception {
        if (Objects.isNull(interceptor.getApiKey()))
            interceptor.setApiKey(Arrays.asList(env.getProperty("apiKeys").split(",")));
        // 方法名为 getDate，参数有 listMap 和 response，其中 listMap 的类型为 List<Map<String,String>>，表示请求体的数据为一个键值对的列表，response 表示响应体
        response.setHeader("Access-Control-Allow-Origin", "*");
        // 设置响应头的访问控制允许所有来源访问
        response.setContentType("text/event-stream");
        // 设置响应头的内容类型为 text/event-stream，表示该响应是一个事件流
        response.setCharacterEncoding("UTF-8");
        // 设置响应的字符编码为 UTF-8
        response.setStatus(200);
        // 设置响应状态码为 200，表示请求已成功处理
        // 遍历 listMap，将用户输入的内容转换为 Message 对象，最后存入 messages
        List<Message> messages = new ArrayList<>();
        if (listMap.get(0).get("role") == null) {
//            弹出listMap的第一个元素
            Map<String, String> settings = listMap.remove(0);
//            从settings中获取temperature 如果没有就设置为0.5
            temperature = settings.get("temperature") == null ? 0.5f : Float.parseFloat(settings.get("temperature"));
            prompt = settings.get("prompt") == null ? "" : settings.get("prompt");

        }

        listMap.forEach(map -> messages.add(
                Message.builder()
                        .role(getCharacter(map.get("role")))
                        .content(map.get("content"))
                        .build()));


        if(!prompt.equals("")){//如果prompt不为空，就在messages的倒数第二个位置插入一个Message对象
            messages.add(messages.size() - 1,
                    Message.builder()
                            .role(getCharacter("system"))
                            .content(prompt)
                            .build());
        }

        //如果prompt不为空，就在messages的倒数第一个位置的content后面加上prompt
//        if (!prompt.equals("")) {
//            messages.get(messages.size() - 1).setContent(prompt + messages.get(messages.size() - 1).getContent());
//        }

        //如果prompt不为空，就在messages的倒数第一个
        // 调用 chat 方法，传入 messages 和一个 ChatEventSourceListener 对象作为参数
        chat(messages, response.getWriter());

    }


    private void chat(List<Message> messages, PrintWriter writer) {
        // 方法名为 chat，参数有 messages 和 listener，其中 messages 的类型为 List<Message>，表示用户输入的信息，listener 的类型为 EventSourceListener，表示事件源监听器，用于监控聊天过程
        OpenAiStreamClient client = OpenAiStreamClient.builder()
                .authInterceptor(interceptor)
//                .apiKey(List.of("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","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","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","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"))
                .apiKey(interceptor.getApiKey())
                // 自定义 key 的获取策略：默认 KeyRandomStrategy
                .keyStrategy(new KeyRandomStrategy())
//          .keyStrategy(new FirstKeyStrategy())
                // 自己做了代理就传代理地址，没有可不不传
//          .apiHost("https://自己代理的服务器地址/")
                .apiHost("https://chatgpt-proxy.lss233.com/")
                .build();

        // 创建一个 OpenAiStreamClient 对象，表示 OpenAI 的聊天模型客户端。可以通过该客户端调用 OpenAI 的机器学习模型，实现智能聊天的功能
        // 聊天模型：gpt-3.5
//      Message message = Message.builder().role(Message.Role.USER).content("Hello").build();
        ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .model(ChatCompletion.Model.GPT_3_5_TURBO.getName())
                .maxTokens(2048)
                .messages(messages)
                .temperature(temperature)
                .build();
        // 创建一个 ChatCompletion 对象，表示聊天的完成情况。其中 model 表示聊天需要使用的机器学习模型，maxTokens 表示每次聊天生成的最大 token 数量，messages 表示用户输入的信息
        CountDownLatch countDownLatch = new CountDownLatch(1);
        client.streamChatCompletion(chatCompletion, new ChatEventSourceListener(writer, countDownLatch));
        // 通过 OpenAI 的客户端对象 client 调用 streamChatCompletion 方法，实现智能聊天的功能。其中 chatCompletion 表示聊天的完成情况，listener 表示事件源监听器，用于监控聊天过程
        // 创建一个 CountDownLatch 对象，表示一个线程等待其他线程完成后继续执行

        try {
            countDownLatch.await();
            System.out.println("聊天结束");
        } catch (InterruptedException e) {
            System.out.println("聊天过程中出现异常");
            e.printStackTrace();
        }
    }

    // 根据用户输入的角色，返回 Message.Role 对象
    private Message.Role getCharacter(String character) {
        if (character.equals("system")) return Message.Role.SYSTEM;
        if (character.equals("user")) return Message.Role.USER;
//        if(character.equals("ai") || character.equals("assistant"))
        return Message.Role.ASSISTANT;
    }
}
