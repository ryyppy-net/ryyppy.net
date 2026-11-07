package drinkcounter.web.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The address apps are authorized for. It serves no MCP tools, so every authorized request gets 501. */
@RestController
public class McpController {

    @RequestMapping({"/mcp", "/mcp/**"})
    public ResponseEntity<String> mcp() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Ryyppy.net has no MCP tools yet.\n");
    }
}
