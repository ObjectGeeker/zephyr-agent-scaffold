package com.object.ai.file.client;

import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.file.model.request.FileUploadRequestVO;
import com.object.ai.file.model.po.FilePO;
import com.object.ai.file.model.vo.FileVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.content.Media;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileStorageClient {

    /**
     * 存储类型
     *
     * @return 存储类型
     */
    String storageType();

    /**
     * 上传文件
     *
     * @param file    文件
     * @param request 附加请求
     * @return 文件对象
     */
    default FileVO put(MultipartFile file, FileUploadRequestVO request) {
        return null;
    }

    /**
     * 上传文件
     *
     * @param file    文件
     * @param request 附加请求
     * @return 文件对象
     */
    default FileVO put(File file, FileUploadRequestVO request) {
        return null;
    }

    /**
     * 删除文件
     *
     * @param bucket    存储块
     * @param objectKey 存储对象的key
     */
    default void remove(String bucket, String objectKey) {

    }

    /**
     * 删除文件
     *
     * @param objectKey 存储对象的key
     */
    default void remove(String objectKey) {

    }

    /**
     * 写入文件流
     *
     * @param filePO   文件元数据
     * @param response HTTP 响应
     */
    default void writeToResponse(FilePO filePO, HttpServletResponse response) {

    }

    /**
     * 将文件解析为可供大模型访问的多模态内容
     * <p>
     * 本地存储直接读取文件字节（Resource），云端存储返回带过期时间的临时访问地址（URI）。
     *
     * @param filePO 文件元数据
     * @return Spring AI 多模态内容
     */
    default Media resolveMedia(FilePO filePO) {
        throw new BusinessException(BizErrorCode.OPERATION_ERROR, "该存储类型不支持多模态访问");
    }

}
