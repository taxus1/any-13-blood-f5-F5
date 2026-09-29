package com.somepro.domain.crossmatch.model;

import com.somepro.common.exception.BizException;

/**
 * 配血方法（领域枚举）：MAJOR 主侧 / MINOR次侧 / BOTH 双侧。
 *
 * 只认这三个值，非法值抛业务异常给出明确取值范围。
 */
public enum CrossmatchMethod {
    MAJOR,
    MINOR,
    BOTH;

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
