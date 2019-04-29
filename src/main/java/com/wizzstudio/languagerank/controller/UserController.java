package com.wizzstudio.languagerank.controller;

/*
Created by Ben Wen on 2019/3/9.
*/

import com.alibaba.fastjson.JSONObject;
import com.wizzstudio.languagerank.constants.Constant;
import com.wizzstudio.languagerank.domain.Award;
import com.wizzstudio.languagerank.domain.User.User;
import com.wizzstudio.languagerank.dto.UserDTO;
import com.wizzstudio.languagerank.enums.StudyPlanDayEnum;
import com.wizzstudio.languagerank.service.*;
import com.wizzstudio.languagerank.util.RedisUtil;
import com.wizzstudio.languagerank.util.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class UserController implements Constant {

    @Autowired
    UserService userService;
    @Autowired
    LanguageCountService languageCountService;
    @Autowired
    StudyPlanService studyPlanService;
    @Autowired
    ShareDimensionCodeService shareDimensionCodeService;
    @Autowired
    AwardService awardService;
    @Autowired
    RedisUtil redisUtil;

    @PostMapping("/userinfo")
    public ResponseEntity getUserInfo(@RequestBody JSONObject jsonObject) {
        Integer userId = jsonObject.getInteger("userId");

//        User user =  userService.findByUserId(userId);
        User user = redisUtil.getUser(userId);
//        System.out.println(user);
        String myLanguage = user.getMyLanguage();
        UserDTO userDTO = new UserDTO();

        // 先新增用户学习计划天数，核心思想是数据库中存储的学习计划天数是用户可见的天数
        if (!"未加入".equals(myLanguage)) {
            if (!user.getStudyPlanDay().equals(StudyPlanDayEnum.ACCOMPLISHED) && user.getIsLogInToday().equals(false)) {
                StudyPlanDayEnum newStudyPlanDay =  userService.updateStudyPlanDay(user);
                userDTO.setIsViewedStudyPlan(true);

                // 当用户今天登录后studyPlanDay与isLogInToday要变化，修改后存入redis(isLogInToday要手动修改)
                user.setStudyPlanDay(newStudyPlanDay);
            } else {
                userDTO.setIsViewedStudyPlan(false);
            }

            Integer[] languageCountArrays = languageCountService.findJoinedNumberByLanguage(myLanguage);
            userDTO.setMyLanguage(myLanguage);
            userDTO.setJoinedNumber(languageCountArrays[0]);
            userDTO.setJoinedToday(languageCountArrays[1]);
            userDTO.setStudyPlanDay(user.getStudyPlanDay().getStudyPlanDay());
            userDTO.setIsViewedJoinMyApplet(user.getIsViewedJoinMyApplet());
        }
        // 用户今天已登录
        if (user.getIsLogInToday().equals(false)) {
            userService.updateIsLogInToday(userId);
            user.setIsLogInToday(true);
        }

        redisUtil.setUser(userId, user);
        log.info("获取"+ userId + "号用户信息成功");
        return ResultUtil.success(userDTO);
    }

    @PostMapping("/myaward")
    public ResponseEntity getMyAward(@RequestBody JSONObject jsonObject) {
        Integer userId = jsonObject.getInteger("userId");

//        User user =  userService.findByUserId(userId);
        User user = redisUtil.getUser(userId);
        Map<String, Object> myAward = new HashMap<>();

        Award studyingLanguage = awardService.findAwardByLanguageName(user.getMyLanguage());
        // 将该语言的奖励返回，但只有完成了学习计划才能显示
        if (user.getStudyPlanDay().equals(StudyPlanDayEnum.ACCOMPLISHED)) {
            studyingLanguage.setIsViewed(true);
        } else {
            studyingLanguage.setIsViewed(false);
        }

        List<Award> studyedLanguage = userService.findStudyedLanguageAwardByUserId(user);

        try {
            myAward.put("studyingLanguage", studyingLanguage);
            myAward.put("studyedLanguage", studyedLanguage);
        } catch (NullPointerException e) {
            log.error("获取"+ userId + "号用户我的奖励失败");
            e.printStackTrace();
            return ResultUtil.error("获取"+ userId + "号用户我的奖励失败");
        }
        log.info("获取"+ userId + "号用户我的奖励成功");
        return ResultUtil.success("获取"+ userId + "号用户我的奖励成功", myAward);
    }

    @PostMapping("/updatelanguage")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity updateLanguage(@RequestBody JSONObject jsonObject){
        Integer userId = jsonObject.getInteger("userId");
        String languageName = jsonObject.getString("languageName");

        Boolean isInStudyPlanLanguage = false;
        for (String language : Constant.STUDY_PLAN_LANGUAGE) {
            if (language.equals(languageName)) {
                isInStudyPlanLanguage = true;
                break;
            }
        }
        if (!isInStudyPlanLanguage) {
            return ResultUtil.error(Constant.NOT_READY_LANGUAGE);
        }

//        User user = userService.findByUserId(userId);
        User user = redisUtil.getUser(userId);
        if (languageName.equals(user.getMyLanguage())) {
            return ResultUtil.error(Constant.STUDYING_NOW);
        }

        try {
            userService.updateMyLanguage(user, languageName);
        } catch (Exception e) {
            log.error(userId + "号用户更新语言失败");
            e.printStackTrace();
            return ResultUtil.error();
        }
        log.info(userId + "号更新语言成功");
        return ResultUtil.success();
    }

    @PostMapping("/studyplan")
    public ResponseEntity getStudyPlan(@RequestBody JSONObject jsonObject){
        Integer userId = jsonObject.getInteger("userId");

//        User user =  userService.findByUserId(userId);
        User user = redisUtil.getUser(userId);
        if (user != null) {
            String languageName = user.getMyLanguage();
            Integer studyPlanDay = user.getStudyPlanDay().getStudyPlanDay();

            Map<String, Object> map = new HashMap<>();
            map.put("studyPlan", studyPlanService.getStudyedStudyPlanDay(languageName,studyPlanDay));
            map.put("isTranspondedList", userService.getUseTranspond(languageName, userId));

            log.info("获取"+ userId + "号用户学习计划成功");
            return ResultUtil.success(map);
        }else {
            log.error("获取"+ userId + "号用户学习计划失败");
            return ResultUtil.error();
        }
    }

    @PostMapping("/updatetranspond")
    public ResponseEntity updateUserTranspond(@RequestBody JSONObject jsonObject) {
        Integer studyPlanDay = jsonObject.getInteger("studyPlanDay");
        Integer userId = jsonObject.getInteger("userId");

        try {
//            User user =  userService.findByUserId(userId);
            User user = redisUtil.getUser(userId);
            userService.updateUserTranspondTable(user, studyPlanDay);
        } catch (Exception e) {
            log.error("更新"+ userId + "号用户转发表失败");
            return ResultUtil.error();
        }
        log.info("更新"+ userId + "号用户转发表成功");
        return ResultUtil.success();
    }

    @PostMapping("/updateisviewedjoinmyapplet")
    public ResponseEntity updateIsViewedJoinMyApplet(@RequestBody JSONObject jsonObject) {
        Integer userId = jsonObject.getInteger("userId");
        userService.updateIsViewedJoinMyApplet(userId);
        User user = redisUtil.getUser(userId);
        user.setIsViewedJoinMyApplet(false);
        redisUtil.setUser(userId, user);

        log.info(userId + "号用户加入我的小程序弹窗不再弹出");
        return ResultUtil.success();
    }

    @PostMapping("/updateuserrelationship")
    public ResponseEntity updateUserRelationship(@RequestBody JSONObject jsonObject) {
        Integer userOne = jsonObject.getInteger("userOne");
        Integer userTwo = jsonObject.getInteger("userTwo");

        try {
            redisUtil.setUserRelationship(userOne, userTwo);
        } catch (Exception e) {
            log.error(userOne + "号用户与"+ userTwo + "号用户新增好友关系失败");
            e.printStackTrace();
            return ResultUtil.error();
        }
        return ResultUtil.success();
    }

    @PostMapping("/getuserrelationship")
    public ResponseEntity getUserRelationship(@RequestBody JSONObject jsonObject) {
        Integer userId = jsonObject.getInteger("userId");

        List<String> stringList = new ArrayList<>(redisUtil.getUserRelationship(userId));
        List<Integer> integerList = null;

        // Java8 stream流式计算
        try {
            integerList = stringList.stream().map(Integer::parseInt).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取"+ userId + "号用户好友关系失败");
            e.printStackTrace();
            return ResultUtil.error();
        }
        log.info("获取"+ userId + "号用户好友关系成功");
        return ResultUtil.success(integerList);
    }

////      获得分享的二维码图片
//    @GetMapping("/dimensioncode")
//    public ResponseEntity shareDimensionCode(){
//        log.info("获取小程序码成功");
//        return ResultUtil.success(shareDimensionCodeService.getDimensionCode());
//    }

//    @PostMapping("/test")
//    public ResponseEntity testSetUser(@RequestBody JSONObject jsonObject) {
//        Integer userId = jsonObject.getInteger("userId");
//        User user = userService.findByUserId(userId);
//        User user = redisUtil.getUser(userId);
//        redisUtil.setUser(userId, user);
//        return ResultUtil.success(redisUtil.getUser(userId));
//    }
}
