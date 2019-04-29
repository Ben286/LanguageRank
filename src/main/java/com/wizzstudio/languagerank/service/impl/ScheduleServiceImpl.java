package com.wizzstudio.languagerank.service.impl;

/*
Created by Ben Wen on 2019/4/17.
*/

import com.wizzstudio.languagerank.dao.UserDAO.UserDAO;
import com.wizzstudio.languagerank.dao.UserDAO.UserStudyedLanguageDAO;
import com.wizzstudio.languagerank.enums.PunchReminderTimeEnum;
import com.wizzstudio.languagerank.service.*;
import com.wizzstudio.languagerank.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    UserDAO userDAO;
    @Autowired
    UserStudyedLanguageDAO userStudyedLanguageDAO;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    LanguageCountService languageCountService;
    @Autowired
    FixedRankService fixedRankService;
    @Autowired
    LanguageHomePageService languageHomePageService;
    @Autowired
    EmployeeService employeeService;
    @Autowired
    EmployeeRankService employeeRankService;
    @Autowired
    PushMessageService pushMessageService;

    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void updateAllIsLogInToDay() {
        userDAO.resetIsLogInToday();
        userStudyedLanguageDAO.resetIsStudyedToday();
        redisUtil.flushUserCacheRedis();
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void updateNumber() {
        languageCountService.updateNumber();
        fixedRankService.resetList();
        languageHomePageService.resetMap();
    }

    @Override
    @Scheduled(cron = "0 55 23 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void saveFixedRankExponent() {
        fixedRankService.saveExponent();
    }

    @Override
    @Scheduled(cron = "0 0 0 * * 1")
    @Transactional(rollbackFor = Exception.class)
    public void resetEmployeeList() {
        employeeService.resetList();
        employeeRankService.resetList();
    }

    @Override
    @Scheduled(cron = "0 55 23 * * 1")
    @Transactional(rollbackFor = Exception.class)
    public void saveEmployeeRankExponent() {
        employeeRankService.saveExponent();
    }

    @Override
    @Scheduled(cron = "0 0 8 * * ?")
    public void pushMessageAtEight() {
        pushMessageService.sendTemplateMsg(PunchReminderTimeEnum.EIGHT);
    }

    @Override
    @Scheduled(cron = "0 0 9 * * ?")
    public void pushMessageAtNine() {
        pushMessageService.sendTemplateMsg(PunchReminderTimeEnum.NINE);
    }

    @Override
    @Scheduled(cron = "0 0 10 * * ?")
    public void pushMessageAtTen() {
        pushMessageService.sendTemplateMsg(PunchReminderTimeEnum.TEN);
    }

    @Override
    @Scheduled(cron = "0 0 11 * * ?")
    public void pushMessageAtEleven() {
        pushMessageService.sendTemplateMsg(PunchReminderTimeEnum.ELEVEN);
    }
}
