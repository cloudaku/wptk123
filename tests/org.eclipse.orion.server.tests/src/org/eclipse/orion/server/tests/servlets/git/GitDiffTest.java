		request = getPostGitDiffRequest(gitDiffUri + "/test.txt", Constants.HEAD);
	private static WebRequest getPostGitDiffRequest(String location, String str) throws JSONException, UnsupportedEncodingException {
		JSONObject body = new JSONObject();
		body.put(GitConstants.KEY_COMMIT_NEW, str);
		str = body.toString();