DELIMITER ;;
DROP TRIGGER  IF EXISTS UPDATE_REMOVED_PROXY_TRIGGER;;
CREATE TRIGGER UPDATE_REMOVED_PROXY_TRIGGER 
  AFTER UPDATE ON PROXY 
  FOR EACH ROW
BEGIN
  IF NEW.status = 3 AND NEW.instance_id IS NOT NULL AND NEW.instance_id <> '' THEN
    CALL mysql.lambda_async('arn:aws:lambda:ap-northeast-1:186165563103:function:proxy-remove-invoker', 
       CONCAT('{ "instance_id" : "', NEW.instance_id, '", "region" : "', IFNULL(NEW.region, ''), '", "status" : "', NEW.status, '" }')
       );
  END IF;
END
;;
DELIMITER ;