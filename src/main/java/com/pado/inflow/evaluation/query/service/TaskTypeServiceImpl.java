package com.pado.inflow.evaluation.query.service;

import com.pado.inflow.common.exception.CommonException;
import com.pado.inflow.common.exception.ErrorCode;
import com.pado.inflow.evaluation.query.dto.TaskTypeDTO;
import com.pado.inflow.evaluation.query.repository.TaskTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("queryTaskTypeService")
public class TaskTypeServiceImpl implements TaskTypeService {

    private final TaskTypeMapper taskTypeMapper;

    public TaskTypeServiceImpl(TaskTypeMapper taskTypeMapper) {
        this.taskTypeMapper = taskTypeMapper;
    }

    // 과제 유형 조회
    @Override
    public List<TaskTypeDTO> findAllTaskTypes() {
        List<TaskTypeDTO> TaskTypeList = taskTypeMapper.findAllTaskTypes();
        if (TaskTypeList == null || TaskTypeList.isEmpty()) {
            throw new CommonException(ErrorCode.NOT_FOUND_EVALUATION_POLICY);
        }
        return TaskTypeList;
    }

    @Override
    public TaskTypeDTO findTaskType(Long taskTypeId) {
        TaskTypeDTO selectedTaskType = taskTypeMapper.findTaskTypeByTaskTypeId(taskTypeId);

        if (selectedTaskType == null) {
            throw new CommonException(ErrorCode.NOT_FOUND_TASK_TYPE);
        }
        return selectedTaskType;
    }

}
