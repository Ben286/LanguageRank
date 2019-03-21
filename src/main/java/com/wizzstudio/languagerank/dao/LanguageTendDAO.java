package com.wizzstudio.languagerank.dao;

import com.wizzstudio.languagerank.domain.FixedFinalExponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LanguageTendDAO extends JpaRepository<FixedFinalExponent, Integer > {

//    FixedFinalExponent库时间降序排列去前两个，即今天和昨天的数据
    @Query(nativeQuery = true,value = "select o from FixedFinalExponent o order by o.fixedFinalExponent desc limit 2")
    List<FixedFinalExponent> findByLanguageName(String LanguageName);

}
