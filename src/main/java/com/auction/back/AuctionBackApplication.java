package com.auction.back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 스케줄 기능 활성화
public class AuctionBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuctionBackApplication.class, args);
	}

}
