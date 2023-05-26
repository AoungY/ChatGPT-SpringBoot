package com.example.chat.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;


public class Choice {
    Map<String,String> delta;
    String index;
    @JsonProperty("finish_reason")
    String finishReason;
}
