package com.assignment.cafe.view;

import java.util.Scanner;

/*
 * 사용자 입력을 담당하는 View 클래스입니다.
 *
 * 요구사항 7번: 화면 입력/출력과 데이터 처리를 분리했습니다.
 * 이 클래스는 Scanner로 입력받는 일만 하고, 주문을 저장하거나 검색하지 않습니다.
 *
 * 요구사항 8번(chap13 예외처리)
 * 숫자 자리에 글자를 입력해도 프로그램이 종료되지 않도록
 * NumberFormatException을 catch해서 다시 입력받습니다.
 */
public class InputView {
    /*
     * Scanner는 System.in을 읽는 객체입니다.
     * 프로그램 전체에서 입력 흐름이 꼬이지 않도록 InputView 안에서 하나만 만들어 사용합니다.
     */
    private final Scanner scanner = new Scanner(System.in);

    public int readInt(String message) {
        /*
         * while (true)는 올바른 숫자가 입력될 때까지 계속 반복합니다.
         * 예를 들어 사용자가 "abc"를 입력하면 Integer.parseInt에서 예외가 발생하지만,
         * catch에서 처리하므로 프로그램이 꺼지지 않습니다.
         */
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();

            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("[입력 오류] 숫자를 입력해주세요.");
            }
        }
    }

    public String readLine(String message) {
        /*
         * 빈 문자열은 의미 있는 데이터가 아니므로 다시 입력받습니다.
         * 고객명이 비어 있거나 검색어가 비어 있으면 기능이 애매해지기 때문입니다.
         */
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();

            if (!input.isBlank()) {
                return input.trim();
            }

            System.out.println("[입력 오류] 빈 값은 입력할 수 없습니다.");
        }
    }

    public boolean readYesNo(String message) {
        /*
         * y 또는 n만 허용합니다.
         * 사용자가 다른 값을 입력해도 프로그램은 종료되지 않고 다시 물어봅니다.
         */
        while (true) {
            System.out.print(message + " (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("y")) {
                return true;
            }

            if (input.equals("n")) {
                return false;
            }

            System.out.println("[입력 오류] y 또는 n을 입력해주세요.");
        }
    }

    public void waitForEnter(String message) {
        System.out.print(message);
        scanner.nextLine();
    }
}
