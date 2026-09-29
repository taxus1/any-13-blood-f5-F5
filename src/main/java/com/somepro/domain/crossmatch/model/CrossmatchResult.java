package com.somepro.domain.crossmatch.model;

import com.somepro.common.exception.BizException;

/**
 * 配血结果（领域枚举）：PENDING 待配 / COMPATIBLE 相合 / INCOMPATIBLE 不合。
 *
 * 申请刚提交为 PENDING；判读后只可能落定为 COMPATIBLE / INCOMPATIBLE，不会再回到 PENDING。
 */
public enum CrossmatchResult {
    PENDING,
    COMPATIBLE,
    INCOMPATIBLE;

    /** 解析外部传入的结果码，大小写不敏感；非法值抛业务异常，只认 PENDING / COMPATIBLE / INCOMPATIBLE。 */
    public static CrossmatchResult parse(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("配血结果不能为空，取值 PENDING 待配 / COMPATIBLE 相合 / INCOMPATIBLE 不合");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("配血结果非法：" + code + "，取值 PENDING / COMPATIBLE / INCOMPATIBLE");
        }
    }
}
