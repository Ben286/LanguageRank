package com.wizzstudio.languagerank.dto;


import com.wizzstudio.languagerank.domain.Language;
import lombok.Data;

@Data
public class FinalRankDTO {

//    语言名，语言图标
    private Language language;

    // 语言热度指数
    private Double fixedFinalExponent;

    // 语言热度变化趋势(值在LanguageTendEnum类中列出)
    private Integer languageTend;

}
