package com.iae.dao;

import com.iae.model.Result;
import java.util.List;

public interface IResultDAO {
    void save(Result result);
    List<Result> findByProjectId(int projectId);
    void deleteByProjectId(int projectId);
}
