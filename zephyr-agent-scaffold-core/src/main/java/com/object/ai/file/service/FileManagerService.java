package com.object.ai.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.object.ai.common.request.DataContainer;
import com.object.ai.common.request.PageRequest;
import com.object.ai.file.model.request.FileQueryDTO;
import com.object.ai.file.model.request.FileUpdateDTO;
import com.object.ai.file.model.request.FileUploadRequestVO;
import com.object.ai.file.model.vo.FileVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileManagerService {

    String upload(MultipartFile file, FileUploadRequestVO request);

    String upload(MultipartFile file, FileUploadRequestVO request, Boolean isTemp);

    Page<FileVO> findAllFile(PageRequest<FileQueryDTO> pageRequest);

    Boolean batchSave(DataContainer<FileUpdateDTO> dataContainer);

    void writeFileToResponse(String fileId, HttpServletResponse response);
}
