-- 充电站管理模块数据库初始化脚本
-- 创建充电站、充电桩、车位相关表

-- 充电站表
CREATE TABLE stations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 基本信息 (StationInfo)
    name VARCHAR(100) NOT NULL COMMENT '充电站名称',
    operator VARCHAR(50) NOT NULL COMMENT '运营商',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    description VARCHAR(500) COMMENT '描述信息',
    facilities VARCHAR(200) COMMENT '配套设施',
    
    -- 地理位置 (Location)
    longitude DECIMAL(10, 6) NOT NULL COMMENT '经度',
    latitude DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    address VARCHAR(200) NOT NULL COMMENT '详细地址',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    
    -- 营业时间 (BusinessHours)
    open_time TIME COMMENT '开始营业时间',
    close_time TIME COMMENT '结束营业时间',
    is_24_hours BOOLEAN DEFAULT FALSE COMMENT '是否24小时营业',
    
    -- 状态信息
    status VARCHAR(20) NOT NULL DEFAULT 'OPERATING' COMMENT '充电站状态',
    total_connectors INT DEFAULT 0 COMMENT '总充电桩数',
    available_connectors INT DEFAULT 0 COMMENT '可用充电桩数',
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version BIGINT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    -- 索引
    INDEX idx_station_status (status),
    INDEX idx_station_operator (operator),
    INDEX idx_station_location (longitude, latitude),
    INDEX idx_station_city (city),
    INDEX idx_station_province (province),
    INDEX idx_station_name (name),
    
    -- 约束
    CONSTRAINT chk_station_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_station_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_station_status CHECK (status IN ('OPERATING', 'MAINTENANCE', 'FAULT', 'CLOSED')),
    CONSTRAINT chk_station_connectors CHECK (total_connectors >= 0 AND available_connectors >= 0 AND available_connectors <= total_connectors)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充电站表';

-- 充电桩表
CREATE TABLE connectors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_id BIGINT NOT NULL COMMENT '充电站ID',
    
    -- 充电桩信息 (ConnectorInfo)
    connector_number VARCHAR(20) NOT NULL COMMENT '充电桩编号',
    connector_type VARCHAR(30) NOT NULL COMMENT '充电桩类型',
    max_power DECIMAL(8, 2) NOT NULL COMMENT '最大功率(kW)',
    voltage INT NOT NULL COMMENT '电压(V)',
    current INT NOT NULL COMMENT '电流(A)',
    protocol VARCHAR(20) NOT NULL COMMENT '通信协议',
    
    -- 状态信息
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE' COMMENT '充电桩状态',
    parking_spot_id BIGINT COMMENT '关联车位ID',
    
    -- 预约信息
    reserved_by_user_id BIGINT COMMENT '预约用户ID',
    reserved_at TIMESTAMP NULL COMMENT '预约时间',
    reservation_expires_at TIMESTAMP NULL COMMENT '预约过期时间',
    
    -- 心跳和故障信息
    last_heartbeat_at TIMESTAMP NULL COMMENT '最后心跳时间',
    fault_reason VARCHAR(500) COMMENT '故障原因',
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version BIGINT DEFAULT 0 COMMENT '版本号（乐观锁）',
    
    -- 外键
    FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_connector_station_id (station_id),
    INDEX idx_connector_status (status),
    INDEX idx_connector_parking_spot (parking_spot_id),
    INDEX idx_connector_user (reserved_by_user_id),
    INDEX idx_connector_type (connector_type),
    INDEX idx_connector_power (max_power),
    INDEX idx_connector_heartbeat (last_heartbeat_at),
    
    -- 唯一约束
    UNIQUE KEY uk_connector_station_number (station_id, connector_number),
    
    -- 约束
    CONSTRAINT chk_connector_power CHECK (max_power > 0),
    CONSTRAINT chk_connector_voltage CHECK (voltage > 0),
    CONSTRAINT chk_connector_current CHECK (current > 0),
    CONSTRAINT chk_connector_status CHECK (status IN ('IDLE', 'RESERVED', 'OCCUPIED', 'CHARGING', 'FAULT', 'OFFLINE', 'MAINTENANCE')),
    CONSTRAINT chk_connector_type CHECK (connector_type IN ('GB_T_DC', 'GB_T_AC', 'TESLA_SUPERCHARGER', 'CCS_COMBO', 'CHADEMO')),
    CONSTRAINT chk_connector_protocol CHECK (protocol IN ('OCPP_16', 'OCPP_20', 'SGCC', 'CUSTOM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充电桩表';

-- 车位表
CREATE TABLE parking_spots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_id BIGINT NOT NULL COMMENT '充电站ID',
    spot_number VARCHAR(20) NOT NULL COMMENT '车位编号',
    spot_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD' COMMENT '车位类型',
    
    -- 地锁信息
    has_lock BOOLEAN DEFAULT FALSE COMMENT '是否有地锁',
    lock_status VARCHAR(20) COMMENT '地锁状态',
    lock_timeout_at TIMESTAMP NULL COMMENT '地锁超时时间',
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键
    FOREIGN KEY (station_id) REFERENCES stations(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_parking_spot_station_id (station_id),
    INDEX idx_parking_spot_type (spot_type),
    INDEX idx_parking_spot_lock_status (lock_status),
    
    -- 唯一约束
    UNIQUE KEY uk_parking_spot_station_number (station_id, spot_number),
    
    -- 约束
    CONSTRAINT chk_parking_spot_type CHECK (spot_type IN ('STANDARD', 'LARGE', 'ACCESSIBLE', 'VIP')),
    CONSTRAINT chk_parking_lock_status CHECK (lock_status IS NULL OR lock_status IN ('UP', 'DOWN', 'FAULT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车位表';

-- 添加车位与充电桩的关联外键
ALTER TABLE connectors ADD CONSTRAINT fk_connector_parking_spot 
    FOREIGN KEY (parking_spot_id) REFERENCES parking_spots(id) ON DELETE SET NULL;

-- 创建空间索引（用于地理位置查询优化）
CREATE SPATIAL INDEX idx_station_location_spatial ON stations((POINT(longitude, latitude)));

-- 插入示例数据
INSERT INTO stations (name, operator, contact_phone, description, facilities, 
                     longitude, latitude, address, city, province,
                     open_time, close_time, is_24_hours, status, total_connectors, available_connectors) VALUES
('北京国贸充电站', '国家电网', '010-12345678', '位于国贸CBD核心区域，交通便利', '停车场,便利店,洗手间', 
 116.457, 39.918, '朝阳区建国门外大街1号', '北京市', '北京市',
 '06:00:00', '22:00:00', FALSE, 'OPERATING', 8, 6),

('上海陆家嘴充电站', '特来电', '021-87654321', '陆家嘴金融区快充站', '地下停车场,商场,餐厅', 
 121.505, 31.245, '浦东新区陆家嘴环路1000号', '上海市', '上海市',
 NULL, NULL, TRUE, 'OPERATING', 12, 10),

('深圳科技园充电站', '星星充电', '0755-11111111', '高新技术园区充电服务', '室内停车,WiFi,休息区', 
 113.947, 22.540, '南山区科技园南区深南大道9988号', '深圳市', '广东省',
 '07:00:00', '23:00:00', FALSE, 'OPERATING', 6, 4);

-- 插入充电桩示例数据
INSERT INTO connectors (station_id, connector_number, connector_type, max_power, voltage, current, protocol, status) VALUES
-- 北京国贸充电站
(1, 'A01', 'GB_T_DC', 60.00, 500, 120, 'OCPP_16', 'IDLE'),
(1, 'A02', 'GB_T_DC', 60.00, 500, 120, 'OCPP_16', 'IDLE'),
(1, 'A03', 'GB_T_DC', 120.00, 750, 160, 'OCPP_16', 'CHARGING'),
(1, 'A04', 'GB_T_DC', 120.00, 750, 160, 'OCPP_16', 'IDLE'),
(1, 'B01', 'GB_T_AC', 7.00, 220, 32, 'OCPP_16', 'IDLE'),
(1, 'B02', 'GB_T_AC', 7.00, 220, 32, 'OCPP_16', 'RESERVED'),
(1, 'B03', 'GB_T_AC', 22.00, 380, 58, 'OCPP_16', 'IDLE'),
(1, 'B04', 'GB_T_AC', 22.00, 380, 58, 'OCPP_16', 'FAULT'),

-- 上海陆家嘴充电站
(2, 'C01', 'GB_T_DC', 180.00, 750, 240, 'OCPP_20', 'IDLE'),
(2, 'C02', 'GB_T_DC', 180.00, 750, 240, 'OCPP_20', 'IDLE'),
(2, 'C03', 'GB_T_DC', 180.00, 750, 240, 'OCPP_20', 'CHARGING'),
(2, 'C04', 'GB_T_DC', 180.00, 750, 240, 'OCPP_20', 'IDLE'),
(2, 'D01', 'TESLA_SUPERCHARGER', 250.00, 400, 625, 'CUSTOM', 'IDLE'),
(2, 'D02', 'TESLA_SUPERCHARGER', 250.00, 400, 625, 'CUSTOM', 'IDLE'),

-- 深圳科技园充电站
(3, 'E01', 'GB_T_DC', 90.00, 500, 180, 'OCPP_16', 'IDLE'),
(3, 'E02', 'GB_T_DC', 90.00, 500, 180, 'OCPP_16', 'IDLE'),
(3, 'E03', 'CCS_COMBO', 150.00, 800, 188, 'OCPP_20', 'IDLE'),
(3, 'E04', 'CCS_COMBO', 150.00, 800, 188, 'OCPP_20', 'MAINTENANCE');

-- 插入车位示例数据
INSERT INTO parking_spots (station_id, spot_number, spot_type, has_lock, lock_status) VALUES
-- 北京国贸充电站车位
(1, 'P01', 'STANDARD', TRUE, 'UP'),
(1, 'P02', 'STANDARD', TRUE, 'UP'),
(1, 'P03', 'STANDARD', TRUE, 'DOWN'),
(1, 'P04', 'STANDARD', TRUE, 'UP'),
(1, 'P05', 'ACCESSIBLE', FALSE, NULL),
(1, 'P06', 'VIP', TRUE, 'DOWN'),

-- 上海陆家嘴充电站车位
(2, 'P01', 'STANDARD', TRUE, 'UP'),
(2, 'P02', 'STANDARD', TRUE, 'UP'),
(2, 'P03', 'LARGE', TRUE, 'DOWN'),
(2, 'P04', 'STANDARD', TRUE, 'UP'),
(2, 'P05', 'STANDARD', FALSE, NULL),
(2, 'P06', 'VIP', TRUE, 'UP'),

-- 深圳科技园充电站车位
(3, 'P01', 'STANDARD', TRUE, 'UP'),
(3, 'P02', 'STANDARD', TRUE, 'UP'),
(3, 'P03', 'ACCESSIBLE', FALSE, NULL),
(3, 'P04', 'STANDARD', TRUE, 'FAULT');

-- 更新充电桩的车位关联
UPDATE connectors SET parking_spot_id = 1 WHERE id = 1;
UPDATE connectors SET parking_spot_id = 2 WHERE id = 2;
UPDATE connectors SET parking_spot_id = 3 WHERE id = 3;
UPDATE connectors SET parking_spot_id = 4 WHERE id = 4;
UPDATE connectors SET parking_spot_id = 5 WHERE id = 5;
UPDATE connectors SET parking_spot_id = 6 WHERE id = 6;
UPDATE connectors SET parking_spot_id = 7 WHERE id = 7;
UPDATE connectors SET parking_spot_id = 8 WHERE id = 8;

UPDATE connectors SET parking_spot_id = 9 WHERE id = 9;
UPDATE connectors SET parking_spot_id = 10 WHERE id = 10;
UPDATE connectors SET parking_spot_id = 11 WHERE id = 11;
UPDATE connectors SET parking_spot_id = 12 WHERE id = 12;
UPDATE connectors SET parking_spot_id = 13 WHERE id = 13;
UPDATE connectors SET parking_spot_id = 14 WHERE id = 14;

UPDATE connectors SET parking_spot_id = 15 WHERE id = 15;
UPDATE connectors SET parking_spot_id = 16 WHERE id = 16;
UPDATE connectors SET parking_spot_id = 17 WHERE id = 17;
UPDATE connectors SET parking_spot_id = 18 WHERE id = 18;
