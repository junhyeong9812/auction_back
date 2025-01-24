package com.auction.back.domain.auction.repository;

import com.auction.back.domain.auction.dto.request.AuctionSearchDto;
import com.auction.back.domain.auction.entity.Auction;
import com.auction.back.domain.auction.entity.QAuction;
import com.auction.back.domain.auction.enums.AuctionStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.auction.back.domain.auction.entity.QAuction.auction;

@Repository
@RequiredArgsConstructor
public class AuctionRepositoryCustomImpl implements AuctionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Auction> searchAuctions(AuctionSearchDto searchDto, Pageable pageable) {
        List<Auction> content = queryFactory
                .selectFrom(auction)
                .where(
                        statusEq(searchDto.getStatus()),
                        keywordLike(searchDto.getKeyword())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(auction.id.desc())
                .fetch();

        long total = queryFactory
                .select(auction.count())
                .from(auction)
                .where(
                        statusEq(searchDto.getStatus()),
                        keywordLike(searchDto.getKeyword())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression statusEq(AuctionStatus status) {
        if (status == null) return null;
        return auction.status.eq(status);
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return null;
        return auction.title.containsIgnoreCase(keyword);
        // or title+description:
        // return auction.title.containsIgnoreCase(keyword)
        //       .or(auction.description.containsIgnoreCase(keyword));
    }
}
