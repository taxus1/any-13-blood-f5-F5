package com.somepro.infrastructure.persistence.crossmatch;

import com.somepro.application.crossmatch.port.OperatorPort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 操作人端口适配器（基础设施层）：从 Reactor Context 取登录账号交给应用层。
 */
@Component
public class ReactorOperatorAdapter implements OperatorPort {

    @Override
    public <T> Mono<T> withOperator(Function<String, Mono<T>> action) {
        return Mono.deferContextual(ctx -> action.apply(ReactiveOperatorContext.getOperator(ctx)));
    }
}
