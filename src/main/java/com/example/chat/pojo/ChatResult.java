package com.example.chat.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.awt.*;
import java.awt.Choice;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class ChatResult {
    private String id;
    private String object;
    private String created;
    private String model;
    private List<Choice> choices;
}
