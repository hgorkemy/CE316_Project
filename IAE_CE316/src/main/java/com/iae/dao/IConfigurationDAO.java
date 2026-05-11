package com.iae.dao;

import com.iae.model.Configuration;
import java.util.List;

public interface IConfigurationDAO {
    void save(Configuration config);
    Configuration findById(int id);
    Configuration findByName(String name);
    List<Configuration> findAll();
    void update(Configuration config);
    void delete(int id);
}
