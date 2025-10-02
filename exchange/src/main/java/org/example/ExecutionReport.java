package org.example;

import java.util.List;

public record ExecutionReport(
        List<Trade> trades,
       double totalFilled,
       double avgPrice
)
{
}

