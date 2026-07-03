package com.object.ai.file.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.object.ai.common.request.DataContainer;
import com.object.ai.common.request.PageRequest;
import com.object.ai.common.utils.ResultUtil;
import com.object.ai.common.vo.BaseResponse;
import com.object.ai.file.model.request.FileQueryDTO;
import com.object.ai.file.model.request.FileUpdateDTO;
import com.object.ai.file.model.request.FileUploadRequestVO;
import com.object.ai.file.model.vo.FileVO;
import com.object.ai.file.service.FileManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("file/manager")
@Tag(name = "文件管理接口")
@RequiredArgsConstructor
@SaCheckLogin
public class FileManagerController {

    private final FileManagerService fileManagerService;

    @Operation(summary = "上传临时文件")
    @PostMapping("upload")
    public BaseResponse<String> upload(@RequestPart("file") MultipartFile file,
                                       @RequestPart(value = "request", required = false) FileUploadRequestVO request) {
        return ResultUtil.ok(fileManagerService.upload(file, request));
    }

    @Operation(summary = "分页查询文件列表")
    @PostMapping("findAllFile")
    @SaCheckRole(value = {"ADMIN"})
    public BaseResponse<Page<FileVO>> findAllFile(@RequestBody PageRequest<FileQueryDTO> pageRequest) {
        return ResultUtil.ok(fileManagerService.findAllFile(pageRequest));
    }

    @Operation(summary = "批量保存文件信息")
    @PostMapping("batchSave")
    @SaCheckRole(value = {"ADMIN"})
    public BaseResponse<Boolean> batchSave(@RequestBody DataContainer<FileUpdateDTO> dataContainer) {
        return ResultUtil.ok(fileManagerService.batchSave(dataContainer));
    }

    @Operation(summary = "预览文件")
    @GetMapping("preview/{fileId}")
    public void preview(@PathVariable String fileId, HttpServletResponse response) {
        fileManagerService.writeFileToResponse(fileId, response);
    }
}
