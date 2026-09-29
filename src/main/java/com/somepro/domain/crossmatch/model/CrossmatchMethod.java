package com.somepro.domain.crossmatch.model;

import com.somepro.common.exception.BizException;

/**
 * 配血方法（领域枚举）：MAJOR 主侧 / MINOR 次侧 / BOTH 双侧，三选一。
 *
 * 纯领域类型，落库存枚举名字符串（t_crossmatch.method），对外也只暴露 {@link #name()}。
 */
public enum CrossmatchMethod {
    MAJOR,
    MINOR,
    BOTH;

    /** 解析外部传入的方法码，大小写不敏感；非法值抛业务异常，只认 MAJOR / MINOR / BOTH。 */
    public static CrossmatchMethod parse(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("配血方法不能为空，取值 MAJOR 主侧 / MINOR 次侧 / BOTH 双侧");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("配血方法非法：" + code + "，取值 MAJOR / MINOR / BOTH");
        }
    }
}
