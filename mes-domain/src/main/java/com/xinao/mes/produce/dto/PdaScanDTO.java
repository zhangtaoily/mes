package com.xinao.mes.produce.dto;

import lombok.Data;

/** PDA 扫码入参（制造卡二维码 / 排程单号） */
@Data
public class PdaScanDTO {

    /** 扫码内容：制造卡二维码（=cardNo）或排程单号 */
    private String content;
}
