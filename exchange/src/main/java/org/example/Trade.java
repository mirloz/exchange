package org.example;

public record Trade(long takerId, long makerId, double price, double quantity) {}

