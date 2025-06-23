# 统一业务语言
## 核心实体:
用户 (User)
车辆 (Vehicle)
场站 (Station)
充电桩/充电枪 (Connector)
车位 (Parking Spot)
地锁 (Ground Lock)
订单 (Order)
充电订单 (ChargingOrder)
预约 (Reservation)
钱包 (Wallet)
支付系统 (Payment Service)
支付交易 (PaymentTransaction)
充电桩系统 (UCPO)
App (应用程序)
账户 (Account)
电费 (Electricity Fee)
服务费 (Service Fee)
电价 (Electricity Price)
支付方式 (Payment Method)
支付渠道 (Payment Channel)
停车场 (Parking Lot)
停车记录 (Parking Record)
地理围栏 (Geofence)
充电会话 (Charging Session)
预约资格 (Reservation Eligibility)
爽约记录 (Default Record)
预约限制 (Reservation Limit)

## 订单:
订单类型 (OrderType)
订单状态 (OrderState)
订单金额 (Order Amount)
充电金额 (Charging Amount)
服务费金额 (Service Amount)

## 充电记录:

充电记录ID (RecordId)
开始时间 (Start Time)
结束时间 (End Time)
开始电量 (Begin SOC)
结束电量 (End SOC)
充电功率 (Charging Power)
充电电压 (Charging Voltage)
充电时长 (Charging Duration)
充电电量 (Charging Energy)
充电费用 (Charging Fee)

## 场站(Station):
运营商 (Operator)
地址 (Address)
经纬度 (Longitude / Latitude)
场站状态 (Station Status)
空闲充电桩数量 (Idle Connector Count)
总充电桩数量 (Total Connector Count)


## 车位(Parking Spot):
车位状态 (Parking Spot Status)

## 地锁(Ground Lock):
地锁状态 (Lock Status)

## 支付(Payment):

支付状态 (Payment State)
支付金额 (Payment Amount)
支付时间 (Payment Time)


## 预约限制(Reservation Limit):

月度爽约次数 (Monthly Default Count)
限制状态 (Restriction Status)
限制原因 (Restriction Reason)

## 超时定义:
支付超时时长 (Payment Timeout Duration)
入位超时时长 (Parking Timeout Duration)

## 动作 (Actions):

### 用户动作:

打开App (Open App)
进入地图模式 (Enter Map)
筛选场站 (Filter Stations)
查看场站详情 (View Station Details)
选择充电枪 (Select Connector)
预约充电 (Create Reservation)
取消预约 (Cancel Reservation)
进入场站 (Enter Station)
点击降地锁 (Click Lock Down)
进入车位 (Enter Parking Spot)
插枪 (Plug In Connector)
扫码 (Scan QR Code)
设置目标电量 (Set Target SOC)
启动充电 (Start Charging)
停止充电 (Stop Charging)
选择支付方式 (Select Payment Method)
支付订单 (Pay Order)
授权免密支付 (Authorize Password-free Payment)
出场 (Leave)
联系客服 (Contact Customer Service)

## 系统动作:

检查地锁&枪空闲 (Check Lock & Connector Available)
检查账户下有电车 (Check EV on Account)
检查存在未支付订单 (Check Unpaid Orders)
检查本月已达预约上限 (Check Monthly Reservation Limit)
降锁 (Lock Down)
升锁 (Lock Up)
启动充电 (Start Charging Command)
停止充电 (Stop Charging Command)
处理支付 (Process Payment)
关闭用户预约权限 (Disable User Reservation Permission)
发送App Push (Send App Push)
发送短信提醒 (Send SMS Reminder)
记录爽约 (Record Default)
结算 (Settle)

## 命令 (Commands):

创建预约 (Create Reservation)
取消预约 (Cancel Reservation)
开始充电 (Start Charging)
停止充电 (Stop Charging)
支付 (Pay)
结束会话 (End Session)
离开 (Leave)
降地锁 (Lock Down)
升锁 (Lock Up)

## 状态 (States):

### 订单状态 (Order States):
初始 (Initial)
待支付 (Unpaid)
已支付 (Paid)
已完成 (Completed)
已取消 (Cancelled)

### 预约状态 (Reservation States):
已预约 (Reserved)
已取消 (Cancelled)
已完成 (Finished)

### 支付状态 (Payment Transaction States):
支付中 (Paying)
支付完成 (Paid)
超时未支付 (Payment Timeout)
已取消 (Cancelled)

### 充电状态 (Charging States):
初始 (Initial)
充电中 (Charging)
充电完成 (Charging Finished)
充电停止中 (Charging Stopping)
充电启动中 (Charging Starting)
等待充电 (Ready to Charge)
充电失败 (Charging Failed)
充电已停止 (Charging Stopped)
充电已终止 (Charging Terminated)

### 地锁状态 (Ground Lock States):
升起 (Up) 
降下 (Down) 


### 场站状态 (Station States):
运营中 (Operating)
维护中 (Maintenance)
故障 (Fault)
关闭 (Closed)

### 充电桩状态 (Connector States):
空闲 (Idle)
占用 (Occupied)
充电中 (Charging)
故障 (Fault)
离线 (Offline)
已预约 (Reserved)

### 预约资格状态 (Reservation Eligibility States):
正常 (Normal)
受限 (Restricted)
禁止 (Forbidden)

