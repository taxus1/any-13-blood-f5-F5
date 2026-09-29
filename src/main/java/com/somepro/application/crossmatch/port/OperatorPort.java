package com.somepro.application.crossmatch.port;

import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 当前操作人解析端口（应用层定义，基础设施层实现）。
 *
 * 判配要记下配血人（登录账号）。操作人在 WebFlux 下放在 Reactor Context 里，
 * 应用层不直接依赖基础设施的 ReactiveOperatorContext，经此端口取出。
 */
public interface OperatorPort {

    /**
     * 在当前响应式上下文中取出登录操作人，未登录回落 system，传给后续动作。
     */
    <T> Mono<T> withOperator(Function<String, Mono<T>> action);
}
