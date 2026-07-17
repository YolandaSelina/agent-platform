package com.rk.agent.common.entity;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rk.agent.common.result.Result;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 通用分页结果
 */
@Data
@NoArgsConstructor
@Schema(description = "分页响应")
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "数据列表")
    private List<T> records;
    @Schema(description = "总记录数")
    private Long total;
    @Schema(description = "当前页码")
    private Long current;
    @Schema(description = "每页大小")
    private Long size;
    @Schema(description = "总页数")
    private Long pages;

    public PageResult(List<T> records, Long total, Long current, Long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = (total + size - 1) / size;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0L, 1L, 10L);
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> mapper) {
        List<T> mapped = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(mapped, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(), (long) page.getNumber() + 1, (long) page.getSize());
    }

    public Result<PageResult<T>> toResult() {
        return Result.success(this);
    }
}
