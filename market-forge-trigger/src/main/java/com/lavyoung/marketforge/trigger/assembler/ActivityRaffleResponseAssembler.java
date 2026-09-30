package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityDrawRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityDrawResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityRaffleResponseAssembler {

    ActivityRaffleCommand toCommand(ActivityDrawRequest request);

    ActivityDrawResponse toResponse(ActivityRaffleResult raffle);
}
