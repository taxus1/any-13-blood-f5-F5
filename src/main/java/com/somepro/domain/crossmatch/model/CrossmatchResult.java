package com.somepro.domain.crossmatch.model;

import com.somepro.common.exception.BizException;

/**
 * 配血结果（领域枚举）：
 * PENDING 待配（申请刚递交、还没出结果）/ COMPATIBLE 相合 / INCOMPATIBLE 不合。
 *
 * 只认这三个值。申请一建档固定为 PENDING，判配后由 {@link Crossmatch#judge} 落到
 * COMPATIBLE / INCOMPATIBLE，不接受外部直接指定。
 */
public enum CrossmatchResult {
    PENDING,
    COMPATIBLE,
    INCOMPATIBLE;

    public static CrossmatchResult parse(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("配血结果不能为空，取值 PENDING / COMPATIBLE / INCOMPATIBLE");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("配血结果非法：" + code + "，取值 PENDING / COMPATIBLE / INCOMPATIBLE");
        }
    }
}
