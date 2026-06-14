package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.domain.dto.*;
import com.gateflow.tracker.domain.entity.*;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.exception.ErrorCode;
import com.gateflow.tracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SetupService {

    private final TrackerAppMapper appMapper;
    private final TrackerPageMapper pageMapper;
    private final TrackerBlockMapper blockMapper;
    private final TrackerFunctionMapper functionMapper;

    // ── Apps ──────────────────────────────────────────────

    public List<AppVO> listApps() {
        return appMapper.selectList(new LambdaQueryWrapper<TrackerApp>().orderByAsc(TrackerApp::getId))
                .stream()
                .map(a -> {
                    int pc = pageMapper.selectCount(
                            new LambdaQueryWrapper<TrackerPage>().eq(TrackerPage::getAppId, a.getId())).intValue();
                    return AppVO.builder()
                            .id(a.getId()).appCode(a.getAppCode()).appName(a.getAppName())
                            .description(a.getDescription()).createdAt(a.getCreatedAt())
                            .pageCount(pc)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public AppVO getApp(Long id) {
        TrackerApp a = appMapper.selectById(id);
        if (a == null) throw new BizException(ErrorCode.EVENT_NOT_FOUND, "应用不存在");
        int pc = pageMapper.selectCount(
                new LambdaQueryWrapper<TrackerPage>().eq(TrackerPage::getAppId, id)).intValue();
        return AppVO.builder()
                .id(a.getId()).appCode(a.getAppCode()).appName(a.getAppName())
                .description(a.getDescription()).createdAt(a.getCreatedAt())
                .pageCount(pc)
                .build();
    }

    public AppVO createApp(CreateAppRequest req) {
        TrackerApp a = new TrackerApp();
        a.setAppCode(req.getAppCode());
        a.setAppName(req.getAppName());
        a.setDescription(req.getDescription());
        appMapper.insert(a);
        return getApp(a.getId());
    }

    public void deleteApp(Long id) {
        getApp(id); // exists check
        // cascade delete pages, blocks, functions
        List<TrackerPage> pages = pageMapper.selectList(
                new LambdaQueryWrapper<TrackerPage>().eq(TrackerPage::getAppId, id));
        for (TrackerPage p : pages) {
            deletePageCascade(p.getId());
        }
        appMapper.deleteById(id);
    }

    // ── Pages ──────────────────────────────────────────────

    public List<PageVO> listPages(Long appId) {
        TrackerApp app = appMapper.selectById(appId);
        if (app == null) throw new BizException(ErrorCode.EVENT_NOT_FOUND, "应用不存在");
        return pageMapper.selectList(new LambdaQueryWrapper<TrackerPage>()
                        .eq(TrackerPage::getAppId, appId).orderByAsc(TrackerPage::getId))
                .stream()
                .map(p -> {
                    int bc = blockMapper.selectCount(
                            new LambdaQueryWrapper<TrackerBlock>().eq(TrackerBlock::getPageId, p.getId())).intValue();
                    return PageVO.builder()
                            .id(p.getId()).appId(p.getAppId()).appCode(app.getAppCode())
                            .pageCode(p.getPageCode()).pageName(p.getPageName())
                            .createdAt(p.getCreatedAt())
                            .blockCount(bc)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public PageVO createPage(Long appId, CreatePageRequest req) {
        getApp(appId); // exists check
        TrackerPage p = new TrackerPage();
        p.setAppId(appId);
        p.setPageCode(req.getPageCode());
        p.setPageName(req.getPageName());
        pageMapper.insert(p);
        return PageVO.builder()
                .id(p.getId()).appId(appId).appCode(appMapper.selectById(appId).getAppCode())
                .pageCode(p.getPageCode()).pageName(p.getPageName())
                .createdAt(p.getCreatedAt()).blockCount(0)
                .build();
    }

    public void deletePage(Long id) {
        deletePageCascade(id);
    }

    private void deletePageCascade(Long pageId) {
        List<TrackerBlock> blocks = blockMapper.selectList(
                new LambdaQueryWrapper<TrackerBlock>().eq(TrackerBlock::getPageId, pageId));
        for (TrackerBlock b : blocks) {
            functionMapper.delete(new LambdaQueryWrapper<TrackerFunction>().eq(TrackerFunction::getBlockId, b.getId()));
            blockMapper.deleteById(b.getId());
        }
        pageMapper.deleteById(pageId);
    }

    // ── Blocks ──────────────────────────────────────────────

    public List<BlockVO> listBlocks(Long pageId) {
        if (pageMapper.selectById(pageId) == null)
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "页面不存在");
        return blockMapper.selectList(new LambdaQueryWrapper<TrackerBlock>()
                        .eq(TrackerBlock::getPageId, pageId).orderByAsc(TrackerBlock::getId))
                .stream()
                .map(b -> {
                    int fc = functionMapper.selectCount(
                            new LambdaQueryWrapper<TrackerFunction>().eq(TrackerFunction::getBlockId, b.getId())).intValue();
                    return BlockVO.builder()
                            .id(b.getId()).pageId(b.getPageId())
                            .blockCode(b.getBlockCode()).blockName(b.getBlockName())
                            .createdAt(b.getCreatedAt())
                            .functionCount(fc)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public BlockVO createBlock(Long pageId, CreateBlockRequest req) {
        if (pageMapper.selectById(pageId) == null)
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "页面不存在");
        TrackerBlock b = new TrackerBlock();
        b.setPageId(pageId);
        b.setBlockCode(req.getBlockCode());
        b.setBlockName(req.getBlockName());
        blockMapper.insert(b);
        return BlockVO.builder()
                .id(b.getId()).pageId(pageId)
                .blockCode(b.getBlockCode()).blockName(b.getBlockName())
                .createdAt(b.getCreatedAt()).functionCount(0)
                .build();
    }

    public void deleteBlock(Long id) {
        functionMapper.delete(new LambdaQueryWrapper<TrackerFunction>().eq(TrackerFunction::getBlockId, id));
        blockMapper.deleteById(id);
    }

    // ── Functions ──────────────────────────────────────────

    public List<FunctionVO> listFunctions(Long blockId) {
        if (blockMapper.selectById(blockId) == null)
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "模块不存在");
        return functionMapper.selectList(new LambdaQueryWrapper<TrackerFunction>()
                        .eq(TrackerFunction::getBlockId, blockId).orderByAsc(TrackerFunction::getId))
                .stream()
                .map(f -> FunctionVO.builder()
                        .id(f.getId()).blockId(f.getBlockId())
                        .funcCode(f.getFuncCode()).funcName(f.getFuncName())
                        .createdAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public FunctionVO createFunction(Long blockId, CreateFunctionRequest req) {
        if (blockMapper.selectById(blockId) == null)
            throw new BizException(ErrorCode.EVENT_NOT_FOUND, "模块不存在");
        TrackerFunction f = new TrackerFunction();
        f.setBlockId(blockId);
        f.setFuncCode(req.getFuncCode());
        f.setFuncName(req.getFuncName());
        functionMapper.insert(f);
        return FunctionVO.builder()
                .id(f.getId()).blockId(blockId)
                .funcCode(f.getFuncCode()).funcName(f.getFuncName())
                .createdAt(f.getCreatedAt())
                .build();
    }

    public void deleteFunction(Long id) {
        functionMapper.deleteById(id);
    }
}
