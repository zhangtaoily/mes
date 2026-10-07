package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 复精梳毛球周转车（毛球在制品物流载体；绑定/称重/入库/移库/拆分/出库记移动轨迹） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rep_truck")
public class RepTruck extends BaseEntity {

    /** 车号 */
    private String truckNo;
    /** 状态 */
    private String status;
    /** 所在车间 */
    private Long workshopId;
    /** 批号 */
    private String batchNo;
    /** 原料 */
    private String rawMaterial;
    /** 库位 */
    private String location;
    /** 袋子颜色 */
    private String bagColor;
    /** 毛球数 */
    private Integer ballCount;
    /** 毛重 */
    private BigDecimal grossWeight;
    /** 净重 */
    private BigDecimal netWeight;
    /** 标重 */
    private BigDecimal standardWeight;
    /** 公定回潮 */
    private BigDecimal moistureStandard;
    /** 实测回潮 */
    private BigDecimal moistureActual;
}
