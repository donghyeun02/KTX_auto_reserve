package org.prac.korailreserve.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.prac.korailreserve.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "사용자", description = "코레일 티켓 예매 관련 API")
public class TicketController {
    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/ticket/reserve")
    @Operation(summary = "티켓 예매", description = "코레일 티켓을 예매합니다.",
            responses = {
                    @ApiResponse(description = "성공적으로 예매됨", responseCode = "200", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(description = "예매 실패", responseCode = "400", content = @Content)
            })
    public ResponseEntity<String> korail(
            @Parameter(description = "전화번호", example = "01012345678") @RequestParam String phoneNumber,
            @Parameter(description = "사용자 ID", example = "0123456789") @RequestParam String txtMember,
            @Parameter(description = "비밀번호", example = "password") @RequestParam String txtPwd,
            @Parameter(description = "출발역", example = "서울") @RequestParam String txtGoStart,
            @Parameter(description = "도착역", example = "울산(통도사)") @RequestParam String txtGoEnd,
            @Parameter(description = "선택 월", example = "08") @RequestParam String selMonth,
            @Parameter(description = "선택 일", example = "01") @RequestParam String selDay,
            @Parameter(description = "시작 시간(시)", example = "12") @RequestParam Integer startHour,
            @Parameter(description = "시작 시간(분)", example = "00") @RequestParam Integer startMin,
            @Parameter(description = "종료 시간(시)", example = "14") @RequestParam Integer endHour,
            @Parameter(description = "종료 시간(분)", example = "00") @RequestParam Integer endMin) {

        String formattedPhoneNumber = "+82" + phoneNumber;
        selMonth = String.format("%02d", Integer.parseInt(selMonth));
        selDay = String.format("%02d", Integer.parseInt(selDay));
        startHour = Math.min(23, Math.max(0, startHour));
        startMin = Math.min(59, Math.max(0, startMin));
        endHour = Math.min(23, Math.max(0, endHour));
        endMin = Math.min(59, Math.max(0, endMin));

        // Set date formatting
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime selectedDateTime = LocalDateTime.parse(date.getYear() + "-" + selMonth + "-" + selDay + " " + String.format("%02d", startHour) + ":" + String.format("%02d", startMin), formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(date.getYear() + "-" + selMonth + "-" + selDay + " " + String.format("%02d", endHour) + ":" + String.format("%02d", endMin), formatter);

        if (!isValidMonth(selMonth)) {
            return ResponseEntity.badRequest().body("잘못된 월 값: " + selDay);
        }

        if (!isValidDay(selDay)) {
            return ResponseEntity.badRequest().body("잘못된 날짜 값: " + selDay);
        }

        if (!isValidHour(startHour)) {
            return ResponseEntity.badRequest().body("잘못된 시작 시간 값: " + startHour);
        }

        if (!isValidMinute(startMin)) {
            return ResponseEntity.badRequest().body("잘못된 시작 분 값: " + startMin);
        }

        if (!isValidHour(endHour)) {
            return ResponseEntity.badRequest().body("잘못된 종료 시간 값: " + endHour);
        }

        if (!isValidMinute(endMin)) {
            return ResponseEntity.badRequest().body("잘못된 종료 분 값: " + endMin);
        }

        if (selectedDateTime.isBefore(LocalDateTime.now()) || endDateTime.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("선택한 일시가 현재보다 이전입니다.");
        }

        // Check if the end time is before the start time
        if (endDateTime.isBefore(selectedDateTime)) {
            return ResponseEntity.badRequest().body("종료 시간이 시작 시간보다 이전입니다.");
        }

        return ResponseEntity.ok(ticketService.reserveTicket(formattedPhoneNumber, txtMember, txtPwd, txtGoStart, txtGoEnd, selMonth, selDay, startHour, startMin, endHour, endMin));
    }

    private boolean isValidMonth(String month) {
        try {
            int monthValue = Integer.parseInt(month);
            return monthValue >= 1 && monthValue <= 12;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDay(String day) {
        try {
            int dayValue = Integer.parseInt(day);
            return dayValue >= 1 && dayValue <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidHour(int hour) {
        return hour >= 0 && hour <= 23;
    }

    private boolean isValidMinute(int minute) {
        return minute >= 0 && minute <= 59;
    }
}
