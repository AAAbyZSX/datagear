/*
 * Copyright (c) 2018 datagear.org. All Rights Reserved.
 */

package org.datagear.web.controller;

import java.io.StringReader;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datagear.connection.ConnectionSource;
import org.datagear.dbmodel.DatabaseModelResolver;
import org.datagear.dbmodel.ModelSqlSelectService;
import org.datagear.dbmodel.ModelSqlSelectService.ModelSqlResult;
import org.datagear.management.domain.Schema;
import org.datagear.management.service.SchemaService;
import org.datagear.persistence.support.UUID;
import org.datagear.web.OperationMessage;
import org.datagear.web.convert.ClassDataConverter;
import org.datagear.web.sqlpad.SqlpadExecutionService;
import org.datagear.web.sqlpad.SqlpadExecutionService.CommitMode;
import org.datagear.web.sqlpad.SqlpadExecutionService.ExceptionHandleMode;
import org.datagear.web.sqlpad.SqlpadExecutionService.SqlCommand;
import org.datagear.web.util.SqlScriptParser;
import org.datagear.web.util.SqlScriptParser.SqlStatement;
import org.datagear.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * SQL工作台控制器。
 * 
 * @author datagear@163.com
 *
 */
@Controller
@RequestMapping("/sqlpad")
public class SqlpadController extends AbstractSchemaConnController
{
	public static final int DEFAULT_SQL_RESULTSET_FETCH_SIZE = 20;

	@Autowired
	private ModelSqlSelectService modelSqlSelectService;

	@Autowired
	private DatabaseModelResolver databaseModelResolver;

	@Autowired
	private SqlpadExecutionService sqlpadExecutionService;

	public SqlpadController()
	{
		super();
	}

	public SqlpadController(MessageSource messageSource, ClassDataConverter classDataConverter,
			SchemaService schemaService, ConnectionSource connectionSource, ModelSqlSelectService modelSqlSelectService,
			DatabaseModelResolver databaseModelResolver, SqlpadExecutionService sqlpadExecutionService)
	{
		super(messageSource, classDataConverter, schemaService, connectionSource);
		this.modelSqlSelectService = modelSqlSelectService;
		this.databaseModelResolver = databaseModelResolver;
		this.sqlpadExecutionService = sqlpadExecutionService;
	}

	public ModelSqlSelectService getModelSqlSelectService()
	{
		return modelSqlSelectService;
	}

	public void setModelSqlSelectService(ModelSqlSelectService modelSqlSelectService)
	{
		this.modelSqlSelectService = modelSqlSelectService;
	}

	public DatabaseModelResolver getDatabaseModelResolver()
	{
		return databaseModelResolver;
	}

	public void setDatabaseModelResolver(DatabaseModelResolver databaseModelResolver)
	{
		this.databaseModelResolver = databaseModelResolver;
	}

	public SqlpadExecutionService getSqlpadExecutionService()
	{
		return sqlpadExecutionService;
	}

	public void setSqlpadExecutionService(SqlpadExecutionService sqlpadExecutionService)
	{
		this.sqlpadExecutionService = sqlpadExecutionService;
	}

	@RequestMapping("/{schemaId}")
	public String index(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model springModel, @PathVariable("schemaId") String schemaId) throws Throwable
	{
		new VoidSchemaConnExecutor(request, response, springModel, schemaId, true)
		{
			@Override
			protected void execute(HttpServletRequest request, HttpServletResponse response, Model springModel,
					Schema schema) throws Throwable
			{
			}
		}.execute();

		String sqlpadId = generateSqlpadId(request, response);
		String sqlpadChannelId = this.sqlpadExecutionService.getSqlpadChannelId(sqlpadId);

		springModel.addAttribute("sqlpadId", sqlpadId);
		springModel.addAttribute("sqlpadChannelId", sqlpadChannelId);

		return "/sqlpad/sqlpad";
	}

	@RequestMapping(value = "/{schemaId}/execute", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> executeSql(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model springModel, @PathVariable("schemaId") String schemaId,
			@RequestParam("sqlpadId") String sqlpadId, @RequestParam("sql") String sql,
			@RequestParam(value = "sqlStartRow", required = false) Integer sqlStartRow,
			@RequestParam(value = "sqlStartColumn", required = false) Integer sqlStartColumn,
			@RequestParam(value = "commitMode", required = false) CommitMode commitMode,
			@RequestParam(value = "exceptionHandleMode", required = false) ExceptionHandleMode exceptionHandleMode,
			@RequestParam(value = "overTimeThreashold", required = false) Integer overTimeThreashold,
			@RequestParam(value = "resultsetFetchSize", required = false) Integer resultsetFetchSize) throws Throwable
	{
		Schema schema = getSchemaNotNull(request, response, schemaId);

		SqlScriptParser sqlScriptParser = new SqlScriptParser(new StringReader(sql));
		if (sqlStartRow != null)
			sqlScriptParser.setContextStartRow(sqlStartRow);
		if (sqlStartColumn != null)
			sqlScriptParser.setContextStartColumn(sqlStartColumn);

		if (commitMode == null)
			commitMode = CommitMode.AUTO;

		if (exceptionHandleMode == null)
			exceptionHandleMode = ExceptionHandleMode.ABORT;

		if (overTimeThreashold == null)
			overTimeThreashold = 10;
		else if (overTimeThreashold < 1)
			overTimeThreashold = 1;
		else if (overTimeThreashold > 60)
			overTimeThreashold = 60;

		if (resultsetFetchSize == null)
			resultsetFetchSize = DEFAULT_SQL_RESULTSET_FETCH_SIZE;

		List<SqlStatement> sqlStatements = sqlScriptParser.parse();

		this.sqlpadExecutionService.submit(sqlpadId, schema, sqlStatements, commitMode, exceptionHandleMode,
				overTimeThreashold, resultsetFetchSize, WebUtils.getLocale(request));

		return buildOperationMessageSuccessEmptyResponseEntity();
	}

	@RequestMapping(value = "/{schemaId}/command", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> command(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model springModel, @PathVariable("schemaId") String schemaId,
			@RequestParam("sqlpadId") String sqlpadId, @RequestParam("command") SqlCommand sqlCommand) throws Throwable
	{
		this.sqlpadExecutionService.command(sqlpadId, sqlCommand);

		return buildOperationMessageSuccessEmptyResponseEntity();
	}

	@RequestMapping(value = "/{schemaId}/select", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ModelSqlResult select(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model springModel, @PathVariable("schemaId") String schemaId,
			@RequestParam("sqlpadId") String sqlpadId, @RequestParam("sql") final String sql,
			@RequestParam(value = "startRow", required = false) Integer startRow,
			@RequestParam(value = "fetchSize", required = false) Integer fetchSize,
			@RequestParam(value = "returnModel", required = false) Boolean returnModel) throws Throwable
	{
		if (startRow == null)
			startRow = 1;
		if (fetchSize == null)
			fetchSize = DEFAULT_SQL_RESULTSET_FETCH_SIZE;
		if (returnModel == null)
			returnModel = false;

		if (fetchSize < 1)
			fetchSize = 1;
		if (fetchSize > 1000)
			fetchSize = 1000;

		final int startRowFinal = startRow;
		final int fetchSizeFinal = fetchSize;

		ModelSqlResult modelSqlResult = new ReturnSchemaConnExecutor<ModelSqlResult>(request, response, springModel,
				schemaId, true)
		{
			@Override
			protected ModelSqlResult execute(HttpServletRequest request, HttpServletResponse response,
					Model springModel, Schema schema) throws Throwable
			{
				ModelSqlResult modelSqlResult = modelSqlSelectService.select(getConnection(), sql, startRowFinal,
						fetchSizeFinal, databaseModelResolver);

				return modelSqlResult;
			}
		}.execute();

		if (!Boolean.TRUE.equals(returnModel))
			modelSqlResult.setModel(null);

		return modelSqlResult;
	}

	protected String generateSqlpadId(HttpServletRequest request, HttpServletResponse response)
	{
		return UUID.gen();
	}
}
