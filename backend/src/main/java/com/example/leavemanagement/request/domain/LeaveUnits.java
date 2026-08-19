package com.example.leavemanagement.request.domain;
public record LeaveUnits(int value) {
    public LeaveUnits { if(value<0)throw new IllegalArgumentException("units cannot be negative"); }
    public double days(){return value/2.0;}
}

