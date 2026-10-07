package com.xinao.mes.pack.dto;

import lombok.Data;

import java.util.List;

/** 组托入参（一批箱绑到新托号，重复组托的箱子被拒绝） */
@Data
public class PalletizeDTO {

    private Long executeId;
    private List<String> boxCodes;
    /** 标准筒数 */
    private Integer standardTube;
}
