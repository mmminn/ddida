package com.runner.ddida.controller;

import java.awt.print.Pageable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.runner.ddida.dto.QnaDto;
import com.runner.ddida.model.Qna;
import com.runner.ddida.model.Reserve;
import com.runner.ddida.security.MemberPrincipalDetails;
import com.runner.ddida.service.QnaService;
import com.runner.ddida.service.SpaceService;
import com.runner.ddida.vo.SpaceDetailVo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice.OffsetMapping.Sort;

/**
 * @author 박재용
 */

@Controller
@RequiredArgsConstructor
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class UserController {

	@ModelAttribute("user")
	public UserDetails getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
		return userDetails;
	}

	private final SpaceService spaceService;
	private final QnaService qnaService;

	// 문의 목록
	@GetMapping("/qna")
	public String qnaList(@PageableDefault(page = 0, size = 10, sort="qnaNo", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable, String searchKeyword, Model model) {
		Page<Qna> qnaList = qnaService.findAll(pageable);
		
		int nowPage = qnaList.getPageable().getPageNumber() + 1;
		int startPage = Math.max(nowPage - 4, 1);
		int endPage = Math.min(nowPage + 5, qnaList.getTotalPages());
		
		model.addAttribute("qnaList", qnaList);
		model.addAttribute("nowPage", nowPage);
		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);

		return "user/qna/qnaList";
	}

	// 문의 상세
	@GetMapping("/qna/{qnaNo}")
	public String qnaDetail(@PathVariable(name = "qnaNo") Long qnaNo, Model model) {
		qnaService.viewcnt(qnaNo);
		Qna qna = qnaService.findByQnaNo(qnaNo).get();
		
		model.addAttribute("qna", qna);
		model.addAttribute("prev", qnaService.prev(qnaNo));
		model.addAttribute("next", qnaService.next(qnaNo));

		return "user/qna/qnaDetail";
	}

	// 문의 등록 폼
	@GetMapping("/qna/add")
	public String qnaAddForm() {
		return "user/qna/qnaAddForm";
	}
	
	// 문의 등록
	@PostMapping("/qna/add")
	public String addQna(QnaDto qnaDto, @AuthenticationPrincipal MemberPrincipalDetails user, Model model) {
		qnaDto.setUserNo(user.getUserNo());
		
		QnaDto qna = qnaService.save(qnaDto);
		model.addAttribute("qna", qna);
		
		return "redirect:/qna/" + qna.getQnaNo();
	}

	// 예약 내역 목록
	@GetMapping("/mypage/reservation")
	public String reserveList(@AuthenticationPrincipal UserDetails userDetails, Model model) {

		model.addAttribute("user", userDetails);

		return "user/mypage/reserveList";
	}

	@GetMapping("/mypage/reservation/reserveDetail")
	public String reserveDetail() {

		return "user/mypage/reserveDetail";
	}

	@GetMapping("/mypage/userInfo")
	public String userInfo() {

		return "user/mypage/userInfo";
	}

	@GetMapping("/mypage/userInfo/edit")
	public String editPassword() {

		return "user/mypage/editPassword";
	}

	@GetMapping("/sports")
	public String spaceList(@AuthenticationPrincipal UserDetails userDetails, Model model,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "pageSize", defaultValue = "12") int pageSize) {

		model.addAttribute("user", userDetails);
		Map<String, Object> spcaeList = new HashMap<String, Object>();
		spcaeList = spaceService.findSpaceList(page, pageSize);

		model.addAttribute("data", spcaeList.get("dataPage"));
		model.addAttribute("currentPage", spcaeList.get("currentPage"));
		model.addAttribute("totalPages", spcaeList.get("totalPages"));

		return "user/sports/spaceList";
	}

	@GetMapping("/sports/search")
	public String spaceSearch(Model model, @RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "pageSize", defaultValue = "12") int pageSize,
			@RequestParam(name = "sports") String sportsNm) {

		switch (sportsNm) {
		case "soccer":
			sportsNm = "축구장";
			break;
		case "futsal":
			sportsNm = "풋살장";
			break;
		case "tennis":
			sportsNm = "테니스장";
			break;
		case "badminton":
			sportsNm = "배드민턴장";
			break;
		}

		List<String> apiVoIdList = spaceService.findApiVoIdList(page, pageSize);
		ArrayList<SpaceDetailVo> searchSportsList = new ArrayList<>();

		int batchSize = 100;
		int totalIterations = 10;

		for (int j = 0; j < totalIterations; j++) {
			List<String> searchList = new ArrayList<>();
			int startIdx = j * batchSize;
			int endIdx = Math.min((j + 1) * batchSize, apiVoIdList.size());
			for (int i = startIdx; i < endIdx; i++) {
				searchList.add(apiVoIdList.get(i));
			}
			searchSportsList.addAll(spaceService.findSports(searchList, sportsNm));
		}

		model.addAttribute("data", searchSportsList);

		return "user/sports/spaceList";
	}

	/*
	 * @GetMapping("/sports/search") public String spaceSearch(Model model,
	 * 
	 * @RequestParam(name = "page", defaultValue = "1") int page,
	 * 
	 * @RequestParam(name = "pageSize", defaultValue = "12") int pageSize,
	 * 
	 * @RequestParam(name = "search") String search) {
	 * 
	 * Map<String, Object> spcaeList = new HashMap<String, Object>(); spcaeList =
	 * service.findSeachList(page, pageSize, search);
	 * 
	 * System.out.println(spcaeList.get("dataPage"));
	 * 
	 * model.addAttribute("search", search); model.addAttribute("data",
	 * spcaeList.get("dataPage")); model.addAttribute("currentPage",
	 * spcaeList.get("currentPage")); model.addAttribute("totalPages",
	 * spcaeList.get("totalPages"));
	 * 
	 * return "user/sports/spaceList"; }
	 */

	@GetMapping("/sports/{rsrcNo}")
	public String spaceDetail(@PathVariable("rsrcNo") String rsrcNo, Model model) {

		log.info("이거 맞음");

		SpaceDetailVo data = spaceService.findDetail(rsrcNo).get(0);

		System.out.println(data.getAmt1());
		System.out.println(data.getAmt2());
		System.out.println(data.getBnrImgFileUrl());
		System.out.println(data.getBnrImgFileUrlAddr());

		model.addAttribute("data", data);

		return "user/sports/spaceDetail";
	}

	@GetMapping("/sports/{rsrcNo}/reserve")
	public String reserveForm(@PathVariable("rsrcNo") String rsrcNo, Model model) {

		SpaceDetailVo data = spaceService.findDetail(rsrcNo).get(0);

		model.addAttribute("data", data);
		model.addAttribute("rsrcNo", rsrcNo);

		return "user/sports/reserveForm";
	}

	@PostMapping("/sports/{rsrcNo}/reserve/check")
	public String checkForm(@ModelAttribute Reserve reserve, Model model) {

		System.out.println(reserve);

		String phone = reserve.getUserPhoneOne() + "-" + reserve.getUserPhoneTwo() + "-" + reserve.getUserPhoneThr();
		String email = reserve.getUserEmailOne() + "@" + reserve.getUserEmailTwo();

		model.addAttribute("data", reserve);
		model.addAttribute("phone", phone);
		model.addAttribute("email", email);

		return "user/sports/checkForm";
	}

	@PostMapping("/sports/1/complete")
	public String complete() {

		return "user/sports/complete";
	}

}