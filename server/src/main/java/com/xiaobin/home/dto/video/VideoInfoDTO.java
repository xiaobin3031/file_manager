package com.xiaobin.home.dto.video;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VideoInfoDTO {

    private List<ProgramDTO> programs;

    private List<StreamDTO> streams;
}
