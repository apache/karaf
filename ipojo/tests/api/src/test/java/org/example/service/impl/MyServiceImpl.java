package org.example.service.impl;

import org.example.service.MyService;

public class MyServiceImpl implements MyService {
    public double compute(double value) {
	return Math.exp(value * Math.cosh(value));
    }
}
