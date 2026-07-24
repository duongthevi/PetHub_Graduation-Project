package com.dtvi.Pet.Ecommerce.service.interf;

import com.dtvi.Pet.Ecommerce.dto.Response;
import com.dtvi.Pet.Ecommerce.dto.statistics.DashboardResponse;

public interface StatisticsService {

    Response getMonthlyStatistics(int month, int year);
    DashboardResponse getDashboard();
}
