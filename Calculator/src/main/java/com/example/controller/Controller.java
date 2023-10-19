package com.example.controller;



import com.example.calculate.CalculationResult;
import com.example.calculate.ExpressionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;


@RestController
public class Controller {

    private final JdbcTemplate jdbcTemplate;
    @Autowired
    public Controller(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("calculate")
    public CalculationResult calculateExpression(@RequestBody ExpressionRequest request) {
        String expression = request.getExpression();
        double result = evaluateExpression(expression);
        CalculationResult calculationResult = new CalculationResult();
        calculationResult.setResult(result);
        saveHistory(result);
        return calculationResult;
    }
    public void saveHistory(double result) {
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM history", Integer.class);
        if (count >= 10) {
            jdbcTemplate.update("DELETE FROM history ORDER BY timestamp LIMIT 1");
        }
        jdbcTemplate.update("INSERT INTO history (result) VALUES (?)", result);
    }
    @RequestMapping("/history")
    public List<Double> getHistory() {
        System.out.println("read history successfully");
        return jdbcTemplate.queryForList("SELECT result FROM history ORDER BY id DESC LIMIT 10", Double.class);
    }

    public double evaluateExpression(String expression) {
        //Replace elements in an expression so that ScriptEngine can follow a reasonable expression
        expression = expression.replaceAll("exp","Math.exp");
        expression = expression.replaceAll("ln", "Math.log");
        expression = expression.replaceAll("tan", "Math.tan");
        expression = expression.replaceAll("cos", "Math.cos");
        expression = expression.replaceAll("sin", "Math.sin");
        expression = expression.replaceAll("π", "Math.PI");
        System.out.println(expression);
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("js");
            Object result = engine.eval(expression);
            if (result instanceof Number) {
                BigDecimal bd = new BigDecimal(((Number) result).doubleValue()).setScale(7, RoundingMode.HALF_UP);
                return bd.doubleValue();
            } else {
                throw new RuntimeException("Invalid expression result");
            }
        } catch (ScriptException | javax.script.ScriptException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to evaluate expression: " + expression, e);
        }
    }
}