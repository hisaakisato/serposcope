require 'json'
require 'aws-sdk-autoscaling'
require 'aws-sdk-cloudwatch'

MAIN_REGION = 'ap-northeast-1'
MAIN_GROUP = ENV['MAIN_GROUP']
CHECK_ALARM= ENV['CHECK_ALARM']
REGIONS = ENV['REGIONS'].split(',')

def lambda_handler(event:, context:)

  autoscaling = Aws::AutoScaling::Client.new({ region: MAIN_REGION })
  resp = autoscaling.describe_auto_scaling_groups({
    auto_scaling_group_names: [ MAIN_GROUP ]
  })
  group = resp.auto_scaling_groups[0]
  diff = group.desired_capacity - group.min_size
  desired_capacity = group.desired_capacity

  # check alarm state
  if diff > 0
    cloudwatch = Aws::CloudWatch::Client.new({ region: MAIN_REGION })
    resp = cloudwatch.describe_alarms({
      alarm_names: [CHECK_ALARM],
      state_value: "OK",
      max_records: 1
    })
    if resp.metric_alarms.length > 0
      puts "Reset capacity to min."
      diff = 0
    end
  end

  REGIONS.each do |region|
    autoscaling = Aws::AutoScaling::Client.new({ region: region })
    resp = autoscaling.describe_auto_scaling_groups({})

    resp.auto_scaling_groups.each do |group|
      capacity = diff == 0 ? group.min_size : desired_capacity
      if capacity != group.desired_capacity
        autoscaling.set_desired_capacity({
          auto_scaling_group_name: group.auto_scaling_group_name,
          desired_capacity: capacity
        })
        puts "Change desired capacity to #{capacity}: #{group.auto_scaling_group_name} (#{region})"
      end
    end
  end
  puts 'Done'
end
