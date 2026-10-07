package com.xinao.mes.pack.vo;

import com.xinao.mes.pack.domain.model.PkBox;
import com.xinao.mes.pack.domain.model.PkExecute;
import lombok.Data;

import java.util.List;

/** 包装执行详情（主表 + 箱子明细） */
@Data
public class PkExecuteDetailVO {

    private PkExecute execute;

    private List<PkBox> boxes;
}
