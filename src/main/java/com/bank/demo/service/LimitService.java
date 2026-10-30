package com.bank.demo.service;

import java.util.List;

import com.bank.demo.dto.LimitResponse;
import com.bank.demo.dto.SetLimitRequest;

public interface LimitService {

    LimitResponse setLimit(SetLimitRequest request);

    List<LimitResponse> getLimits(String accountFrom);
    
}
