package com.xinao.mes.pack.vo;

import com.xinao.mes.pack.domain.model.PkBox;
import com.xinao.mes.pack.domain.model.PkExecute;
import lombok.Data;

/** PDA 扫箱结果：箱子 + 所属包装执行单 */
@Data
public class PdaBoxScanVO {

    /** 箱子明细 */
    private PkBox box;

    /** 所属包装执行单 */
    private PkExecute execute;
}
