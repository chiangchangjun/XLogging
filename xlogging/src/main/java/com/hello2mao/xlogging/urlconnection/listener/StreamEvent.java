package com.hello2mao.xlogging.urlconnection.listener;

import com.hello2mao.xlogging.urlconnection.TransactionState;

import java.util.EventObject;

public class StreamEvent extends EventObject {

    private Exception exception;
    private TransactionState transactionState;

    public StreamEvent(Object source, TransactionState transactionState) {
        super(source);
        this.transactionState = transactionState;
    }

    public StreamEvent(Object source, TransactionState transactionState, Exception exception) {
        super(source);
        this.transactionState = transactionState;
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public TransactionState getTransactionState() {
        return transactionState;
    }

    public boolean isError() {
        return exception != null;
    }
}
