package com.xinao.mes.produce.vo;

import com.xinao.mes.produce.domain.model.MtFeed;
import com.xinao.mes.produce.domain.model.MtFeedItem;
import lombok.Data;

import java.util.List;

/** 投料记录详情（明细子表） */
@Data
public class FeedDetailVO {

    private MtFeed feed;
    private List<MtFeedItem> items;
}
