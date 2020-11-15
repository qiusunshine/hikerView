package com.paincker.timetracer.tracer;

public interface ITracer {

    ITracer EMPTY_TRACER = new ITracer(){

        @Override
        public void traceStart() {

        }

        @Override
        public void traceEnd() {

        }

        @Override
        public void methodStart(String method) {

        }

        @Override
        public void methodEnd(String method) {

        }
    };

    void traceStart();

    void traceEnd();

    void methodStart(String method);

    void methodEnd(String method);
}
