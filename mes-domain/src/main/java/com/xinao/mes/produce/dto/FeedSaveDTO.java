package com.xinao.mes.produce.dto;

import com.xinao.mes.produce.domain.model.MtFeed;
import com.xinao.mes.produce.domain.model.MtFeedItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 投料保存入参（明细来自生产任务用料清单） */
@Data
public class FeedSaveDTO {

    @NotNull(message = "投料主信息不能为空")
    private MtFeed feed;

    private List<MtFeedItem> items;
}
