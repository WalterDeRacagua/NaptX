package com.example.offlinepaymentsystem.data.blockchain;

import org.web3j.protocol.Web3j;

public class Web3Manager {

    private static final String TAG = "Web3Manager";
    private static  Web3Manager instance;
    private Web3j web3j;
    private boolean isConnected = false;
}
